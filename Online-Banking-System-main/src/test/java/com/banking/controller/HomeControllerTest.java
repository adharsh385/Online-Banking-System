package com.banking.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    private final HomeController controller = new HomeController();

    @Test
    void mapsPublicPagesToTheirTemplates() {
        assertEquals("public/index", controller.index());
        assertEquals("public/index", controller.home());
        assertEquals("public/about", controller.about());
        assertEquals("public/contact", controller.contact());
    }
}
