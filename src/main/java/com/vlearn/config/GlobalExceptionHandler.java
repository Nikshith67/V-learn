package com.vlearn.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.NoSuchElementException;

/**
 * Prevents blank white-label error pages when optional data is missing (e.g. stale session user id).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public RedirectView handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Your session expired or the page could not be loaded. Please sign in again.");
        String ctx = request.getContextPath() != null ? request.getContextPath() : "";
        return new RedirectView(ctx + "/login");
    }
}
