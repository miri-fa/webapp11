package es.urjc.code.daw.marketplace.web.order.controller;

import com.google.common.collect.Lists;
import es.urjc.code.daw.marketplace.domain.Order;
import es.urjc.code.daw.marketplace.domain.Product;
import es.urjc.code.daw.marketplace.domain.User;
import es.urjc.code.daw.marketplace.security.user.UserPrincipal;
import es.urjc.code.daw.marketplace.service.*;
import es.urjc.code.daw.marketplace.util.EmailContent;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Calendar;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final EmailService emailService;
    private final SaleService saleService;
    private final ProductService productService;
    private final PdfExporterService pdfExporterService;

    public OrderController(OrderService orderService,
                           UserService userService,
                           SaleService saleService,
                           EmailService emailService,
                           ProductService productService, PdfExporterService pdfExporterService) {
        this.orderService = orderService;
        this.userService = userService;
        this.emailService = emailService;
        this.saleService = saleService;
        this.productService = productService;
        this.pdfExporterService = pdfExporterService;
    }

    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @RequestMapping(path = "/services", method = RequestMethod.GET)
    public String listServices(@AuthenticationPrincipal UserPrincipal userPrincipal,
                               Model model) {

        model.addAttribute("isServices", true);
        model.addAttribute("isLoggedIn", "yes");
        model.addAttribute("loggedUser", userService.findUserByEmail(userPrincipal.getUsername()));

        if(userPrincipal.getUser().isAdmin()) {
            model.addAttribute("isAdmin", "yes");
        }

        return "services";
    }

    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @GetMapping(path = "/service/{id}")
    public String displayService(@PathVariable("id") Long id,
                                 @AuthenticationPrincipal UserPrincipal userPrincipal,
                                 Model model) {

        model.addAttribute("isService", true);

        User currentUser = userService.findUserByEmail(userPrincipal.getUsername());
        Order order = orderService.findOrderById(id);

        if(!currentUser.isAdmin() && !order.getUser().equals(currentUser)) {
            return "redirect:/error";
        }

        model.addAttribute("isLoggedIn", "yes");
        model.addAttribute("loggedUser", userService.findUserByEmail(userPrincipal.getUsername()));

        if(userPrincipal.getUser().isAdmin()) {
            model.addAttribute("isAdmin", "yes");
        }

        model.addAttribute("orderId", order.getId());
        model.addAttribute("orderCategory", order.getProduct().getCategory());
        model.addAttribute("orderPurchaseDate", order.getCreationDate().toString());
        model.addAttribute("orderExpiryDate", order.getExpiryDate().toString());

        if(order.isExpired()) {
            model.addAttribute("orderIsExpired", order.isExpired());
        }

        return "service";
    }

    /**
     * Exports a pdf detailing the information of the receipt
     * from the order of the purchased server, giving out the id of the order
     * and the id of the user who ordered it.
     */
    
    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @GetMapping("/order/{orderId}/export_pdf")
    public void exportToPDF(HttpServletResponse response,
                            @PathVariable("orderId") Long orderId,
                            @AuthenticationPrincipal UserPrincipal userPrincipal) throws Exception {

        User currentUser = userService.findUserByEmail(userPrincipal.getUsername());
        Order currentOrder = orderService.findOrderById(orderId);

        if (!currentUser.isAdmin() && !currentOrder.getUser().equals(currentUser)){
            throw new RuntimeException("Access Denied");
        }

        response.setContentType("application/pdf");

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=userOrder_" + currentOrder.getId() + "_" + currentOrder.getUser().getId() + ".pdf";
        response.setHeader(headerKey, headerValue);

        pdfExporterService.exportPdf(response, currentOrder);

    }

    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @RequestMapping(path = "/services/{page}/{amount}", method = RequestMethod.GET)
    public String findOrders(@PathVariable("page") Integer page,
                             @PathVariable("amount") Integer amount,
                             @AuthenticationPrincipal UserPrincipal userPrincipal,
                             Model model) {

        User currentUser = userService.findUserByEmail(userPrincipal.getUsername());
        List<Order> orders = orderService.findAllOrdersByUserId(currentUser.getId(), PageRequest.of(page - 1, amount));

        model.addAttribute("orders", orders);

        return "orders";
    }

    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @RequestMapping(path = "/order/{productId}/place", method = RequestMethod.GET)
    public String placeOrder(@PathVariable("productId") Long productId,
                             @AuthenticationPrincipal UserPrincipal userPrincipal) {

        User currentUser = userService.findUserByEmail(userPrincipal.getUsername());
        Product product = productService.findProductById(productId);

        Order order = Order.builder()
                .product(product)
                .finalCost(product.getPrice())
                .user(currentUser)
                .build();

        saleService.applyOtdDiscount(order);
        saleService.applyAdDiscount(order);

        Order savedOrder = orderService.saveOrder(order);

        String message = EmailContent.create()
                .addHeading("Thanks for your purchase, " + currentUser.getFirstName() + " " + currentUser.getSurname())
                .addUnorderedList(
                        "Here is your purchased product information",
                        Lists.newArrayList(
                                "Price: " + product.getPrice(),
                                "Ram: " + product.getRam(),
                                "Cores: " + product.getCores(),
                                "Storage: " + product.getStorage(),
                                "Transfer: " + product.getTransfer()
                        )
                )
                .addHeading("Remember that you can manage each purchased product from my services page!")
            .build();

        emailService.sendEmail(currentUser.getEmail(), "#" + savedOrder.getId() + " Purchase receipt", message);

        return "redirect:/services";
    }

    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @RequestMapping(path = "/order/{orderId}/renew", method = RequestMethod.GET)
    public String renewOrder(@PathVariable("orderId") Long orderId,
                             @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Order order = orderService.findOrderById(orderId);
        User currentUser = userService.findUserByEmail(userPrincipal.getUsername());

        if(!currentUser.isAdmin() && !order.getUser().equals(currentUser)) {
            return "redirect:/error";
        }

        order.setFinalCost(order.getFinalCost() + order.getProduct().getPrice());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date().after(order.getExpiryDate()) ? new Date() : order.getExpiryDate());
        calendar.add(Calendar.MONTH, 1);
        order.setExpiryDate(calendar.getTime());

        orderService.saveOrder(order);

        return "redirect:/service/" + orderId;
    }

    @PreAuthorize("hasAnyRole('ROLE_CLIENT', 'ROLE_ADMIN')")
    @RequestMapping(path = "/order/{orderId}/cancel", method = RequestMethod.GET)
    public String cancelOrder(@PathVariable("orderId") Long orderId,
                              @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Order order = orderService.findOrderById(orderId);
        User currentUser = userService.findUserByEmail(userPrincipal.getUsername());

        if(!currentUser.isAdmin() && !order.getUser().equals(currentUser)) {
            return "redirect:/error";
        }

        order.setExpiryDate(new Date());

        orderService.saveOrder(order);

        return "redirect:/service/" + orderId;
    }

}