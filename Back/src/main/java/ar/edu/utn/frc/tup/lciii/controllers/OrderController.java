package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.OrdersDto;
import ar.edu.utn.frc.tup.lciii.dtos.RequestOrderDto;
import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.UserEntity;
import ar.edu.utn.frc.tup.lciii.repositories.GeneratorRepository;
import ar.edu.utn.frc.tup.lciii.repositories.UserRepository;
import ar.edu.utn.frc.tup.lciii.services.OrderService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@CrossOrigin(origins = "http://localhost:4200")

@RestController
@RequestMapping("Order")
public class OrderController {
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private OrderService orderService;

    @PostMapping("/crear")
    public ResponseEntity<OrdersDto> createOrder(@RequestBody RequestOrderDto r) {
        OrdersDto ordersDto = orderService.createOrder(r);
        return ResponseEntity.ok(ordersDto);
    }
}
