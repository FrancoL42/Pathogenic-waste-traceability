package ar.edu.utn.frc.tup.lciii.services;

import ar.edu.utn.frc.tup.lciii.dtos.OrdersDto;
import ar.edu.utn.frc.tup.lciii.dtos.RequestOrderDto;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {
    OrdersDto createOrder(RequestOrderDto requestOrderDto);
}
