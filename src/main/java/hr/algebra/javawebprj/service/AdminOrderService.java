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
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Order> search(OrderFilterForm filter) {
        String username = filter.getUsername() == null ? "" : filter.getUsername().trim();
        LocalDateTime from = toStart(filter.getFromDate());
        LocalDateTime to = toEnd(filter.getToDate());
        return orderRepository.searchAdmin(username, from, to);
    }

    @Transactional(readOnly = true)
    public List<String> allUsernames() {
        return userRepository.findAll().stream()
                .map(u -> u.getUsername())
                .sorted()
                .distinct()
                .toList();
    }

    private static LocalDateTime toStart(LocalDate date) {
        return date == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : date.atStartOfDay();
    }

    private static LocalDateTime toEnd(LocalDate date) {
        return date == null ? LocalDateTime.of(2099, 12, 31, 23, 59, 59) : date.atTime(LocalTime.MAX);
    }
}
