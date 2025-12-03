package com.ifsp.projetoCantina.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String index() {
        return "index";
    }

    @GetMapping("/identificacaoCliente")
    public String identificacaoCliente() {
        return "identificacaoCliente";
    }

    @GetMapping("/vendedor")
    public String vendedor() {
        return "vendedor";
    }
}
