package hr.algebra.javawebprj.service;

import hr.algebra.javawebprj.dto.OrderFilterForm;
import hr.algebra.javawebprj.model.Order;
import hr.algebra.javawebprj.repository.OrderRepository;
import hr.algebra.javawebprj.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Order> search(OrderFilterForm filter) {
        String username = "";
        if (filter.getUsername() != null) {
            username = filter.getUsername().trim();
        }
        LocalDateTime from = toStart(filter.getFromDate());
        LocalDateTime to = toEnd(filter.getToDate());
        if (username.isBlank()) {
            return orderRepository.findByOrderDateBetweenOrderByOrderDateDesc(from, to);
        }
        return orderRepository.findByUserUsernameContainingIgnoreCaseAndOrderDateBetweenOrderByOrderDateDesc(
                username, from, to);
    }

    @Transactional(readOnly = true)
    public List<String> allUsernames() {
        List<String> names = new ArrayList<>();
        userRepository.findAll().forEach(u -> names.add(u.getUsername()));
        Collections.sort(names);
        return names;
    }

    private static LocalDateTime toStart(LocalDate date) {
        if (date == null) {
            return LocalDateTime.of(1970, 1, 1, 0, 0);
        }
        return date.atStartOfDay();
    }

    private static LocalDateTime toEnd(LocalDate date) {
        if (date == null) {
            return LocalDateTime.of(2099, 12, 31, 23, 59, 59);
        }
        return date.atTime(LocalTime.MAX);
    }
}
