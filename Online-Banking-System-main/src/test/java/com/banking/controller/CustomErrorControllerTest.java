package com.banking.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomErrorControllerTest {

    private final CustomErrorController controller = new CustomErrorController();

    @Test
    void mapsNotFoundToNotFoundTemplate() {
        assertEquals("error/404", handle(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void mapsForbiddenToForbiddenTemplate() {
        assertEquals("error/403", handle(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void mapsInternalServerErrorToServerErrorTemplate() {
        assertEquals("error/500", handle(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @Test
    void usesGenericTemplateForUnknownStatus() {
        assertEquals("error/error", handle(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void usesGenericTemplateWhenStatusIsMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertEquals("error/error", controller.handleError(request, new ExtendedModelMap()));
    }

    private String handle(int statusCode) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
        return controller.handleError(request, new ExtendedModelMap());
    }
}
