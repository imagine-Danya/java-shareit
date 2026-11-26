package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.booking.dto.NewBookingDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    public BookingDto create(Long userId, NewBookingDto dto) {
        User booker = getUser(userId);
        validateDates(dto.getStart(), dto.getEnd());

        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь с id " + dto.getItemId() + " не найдена"));
        if (!item.isAvailable()) {
            throw new ValidationException("Вещь с id " + item.getId() + " недоступна для бронирования");
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может забронировать собственную вещь");
        }

        Booking booking = bookingRepository.save(BookingMapper.toBooking(dto, item, booker));
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public BookingDto approve(Long userId, Long bookingId, boolean approved) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id " + bookingId + " не найдено"));
        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Подтверждать или отклонять бронирование может только владелец вещи");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        getUser(userId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id " + bookingId + " не найдено"));

        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Просматривать бронирование может только автор или владелец вещи");
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getUserBookings(Long userId, String state) {
        return findByState(userId, state, false);
    }

    @Override
    public List<BookingDto> getOwnerBookings(Long userId, String state) {
        return findByState(userId, state, true);
    }

    private List<BookingDto> findByState(Long userId, String state, boolean owner) {
        getUser(userId);
        BookingState bookingState = parseState(state);
        Sort sort = Sort.by(Sort.Direction.DESC, "start");
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> owner
                    ? bookingRepository.findByItemOwnerId(userId, sort)
                    : bookingRepository.findByBookerId(userId, sort);
            case CURRENT -> owner
                    ? bookingRepository.findByItemOwnerIdAndStartIsBeforeAndEndIsAfter(userId, now, now, sort)
                    : bookingRepository.findByBookerIdAndStartIsBeforeAndEndIsAfter(userId, now, now, sort);
            case PAST -> owner
                    ? bookingRepository.findByItemOwnerIdAndEndIsBefore(userId, now, sort)
                    : bookingRepository.findByBookerIdAndEndIsBefore(userId, now, sort);
            case FUTURE -> owner
                    ? bookingRepository.findByItemOwnerIdAndStartIsAfter(userId, now, sort)
                    : bookingRepository.findByBookerIdAndStartIsAfter(userId, now, sort);
            case WAITING -> owner
                    ? bookingRepository.findByItemOwnerIdAndStatus(userId, BookingStatus.WAITING, sort)
                    : bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.WAITING, sort);
            case REJECTED -> owner
                    ? bookingRepository.findByItemOwnerIdAndStatus(userId, BookingStatus.REJECTED, sort)
                    : bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.REJECTED, sort);
        };

        return bookings.stream().map(BookingMapper::toBookingDto).collect(Collectors.toList());
    }

    private BookingState parseState(String state) {
        try {
            return BookingState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown state: " + state);
        }
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        if (start == null || end == null) {
            throw new ValidationException("Даты начала и окончания бронирования обязательны");
        }
        if (end.isBefore(now) || end.isEqual(now)) {
            throw new ValidationException("Дата окончания бронирования не может быть в прошлом");
        }
        if (start.isBefore(now)) {
            throw new ValidationException("Дата начала бронирования не может быть в прошлом");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("Дата начала должна быть раньше даты окончания");
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }
}