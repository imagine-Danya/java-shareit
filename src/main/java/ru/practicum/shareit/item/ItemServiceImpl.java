package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentMapper;
import ru.practicum.shareit.item.dto.ItemDetailDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemMapper;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        checkUserId(userId);
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        Item item = itemRepository.save(ItemMapper.toItem(itemDto, owner));
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        checkUserId(userId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Редактировать вещь может только её владелец");
        }
        if (itemDto.getName() != null) {
            if (itemDto.getName().isBlank()) {
                throw new ValidationException("Название вещи не может быть пустым");
            }
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            if (itemDto.getDescription().isBlank()) {
                throw new ValidationException("Описание вещи не может быть пустым");
            }
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDetailDto getById(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        List<Comment> comments = commentRepository.findAllByItemIdOrderByCreatedDesc(itemId);
        Booking lastBooking = null;
        Booking nextBooking = null;
        if (userId != null && userId.equals(item.getOwner().getId())) {
            LocalDateTime now = LocalDateTime.now();
            lastBooking = bookingRepository
                    .findFirstByItemIdAndStatusAndStartIsBeforeOrderByEndDesc(itemId, BookingStatus.APPROVED, now)
                    .orElse(null);
            nextBooking = bookingRepository
                    .findFirstByItemIdAndStatusAndStartIsAfterOrderByStartAsc(itemId, BookingStatus.APPROVED, now)
                    .orElse(null);
        }
        return ItemMapper.toItemDetailDto(item, lastBooking, nextBooking, comments);
    }

    @Override
    public List<ItemOwnerDto> getAllByOwner(Long userId) {
        checkUserId(userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        return itemRepository.findAllByOwnerIdOrderByIdAsc(userId).stream()
                .map(item -> {
                    LocalDateTime now = LocalDateTime.now();
                    Booking lastBooking = bookingRepository
                            .findFirstByItemIdAndStatusAndStartIsBeforeOrderByEndDesc(item.getId(), BookingStatus.APPROVED, now)
                            .orElse(null);
                    Booking nextBooking = bookingRepository
                            .findFirstByItemIdAndStatusAndStartIsAfterOrderByStartAsc(item.getId(), BookingStatus.APPROVED, now)
                            .orElse(null);
                    List<Comment> comments = commentRepository.findAllByItemIdOrderByCreatedDesc(item.getId());
                    return ItemMapper.toItemOwnerDto(item, lastBooking, nextBooking, comments);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        checkUserId(userId);
        if (commentDto.getText() == null || commentDto.getText().isBlank()) {
            throw new ValidationException("Текст комментария не может быть пустым");
        }
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        boolean rented = bookingRepository.existsByBookerIdAndItemIdAndStatusAndEndIsBefore(
                userId, itemId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!rented) {
            throw new ValidationException("Оставить отзыв может только пользователь, арендовавший вещь, "
                    + "и только после окончания аренды");
        }

        Comment comment = commentRepository.save(CommentMapper.toComment(commentDto, item, author));
        return CommentMapper.toCommentDto(comment);
    }

    private void checkUserId(Long userId) {
        if (userId == null) {
            throw new ValidationException("Не указан идентификатор пользователя в заголовке X-Sharer-User-Id");
        }
    }
}