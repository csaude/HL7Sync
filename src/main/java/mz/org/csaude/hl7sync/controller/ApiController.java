package mz.org.csaude.hl7sync.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/demographics/generate")
    public void demographics(){
        System.out.println("I work");
    }
}
