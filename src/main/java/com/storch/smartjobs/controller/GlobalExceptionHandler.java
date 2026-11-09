package com.storch.smartjobs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public String invalidRequest(IllegalArgumentException exception, Model model, HttpServletResponse response) {
        response.setStatus(400);
        model.addAttribute("message", exception.getMessage());
        model.addAttribute("status", 400);
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(Model model, HttpServletResponse response) {
        response.setStatus(400);
        model.addAttribute("message", "O arquivo enviado ultrapassa o limite de 5 MB.");
        model.addAttribute("status", 400);
        return "error";
    }

}
