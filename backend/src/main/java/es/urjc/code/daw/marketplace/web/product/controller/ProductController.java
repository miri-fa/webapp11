package es.urjc.code.daw.marketplace.web.product.controller;

import es.urjc.code.daw.marketplace.domain.AccumulativeDiscount;
import es.urjc.code.daw.marketplace.domain.OneTimeDiscount;
import es.urjc.code.daw.marketplace.domain.Product;
import es.urjc.code.daw.marketplace.domain.User;
import es.urjc.code.daw.marketplace.security.user.UserPrincipal;
import es.urjc.code.daw.marketplace.service.ProductService;
import es.urjc.code.daw.marketplace.service.SaleService;
import es.urjc.code.daw.marketplace.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class ProductController {

    private final ProductService productService;
    private final SaleService saleService;
    private final UserService userService;

    public ProductController(ProductService productService,
                             SaleService saleService,
                             UserService userService) {
        this.productService = productService;
        this.saleService = saleService;
        this.userService = userService;
    }

    @RequestMapping(path = "/pricing", method = RequestMethod.GET)
    public String productsPricing(@RequestParam(value = "selected", required = false) String categoryToLoad,
                                  @AuthenticationPrincipal UserPrincipal userPrincipal,
                                  Model model) {

        List<Product> products = productService.findAllProducts();
        model.addAttribute("products", products);

        Optional<OneTimeDiscount> oneTimeDiscount = saleService.getCurrentOtd();
        oneTimeDiscount.ifPresent(otd -> {
            model.addAttribute("otd", otd);
            model.addAttribute("otdProduct", productService.findProductById(otd.getProductId()));
        });

        Optional<AccumulativeDiscount> accumulativeDiscount = saleService.getCurrentAd();
        accumulativeDiscount.ifPresent(ad -> {
            model.addAttribute("ad", ad);
            model.addAttribute("adProduct", productService.findProductById(ad.getProductId()));
        });

        if(oneTimeDiscount.isPresent() || accumulativeDiscount.isPresent()) {
            model.addAttribute("hasDiscount", "yes");
        }

        final String viewIndicator = "isPricing";
        model.addAttribute(viewIndicator, "yes");

        if(StringUtils.isNotEmpty(categoryToLoad) && StringUtils.isNotBlank(categoryToLoad)) {
            model.addAttribute("loadCategory", categoryToLoad);
        }

        return "pricing";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(path = "/panel")
    public String panel(Model model) {

        List<Pair<String, Integer>> weeklyCategoryPurchases = productService.findCategoryToWeeklyPurchases();
        model.addAttribute("weeklyCategoryPurchases", weeklyCategoryPurchases);

        List<Integer> salesPerDayInWeek = productService.findSalesPerDayInWeek();
        model.addAttribute("salesPerDayInWeek", salesPerDayInWeek);

        Long accumulatedCapital = productService.findAccumulatedCapital();
        model.addAttribute("accumulatedCapital", accumulatedCapital);

        model.addAttribute("isPanel", true);

        return "panel";

    }

}
