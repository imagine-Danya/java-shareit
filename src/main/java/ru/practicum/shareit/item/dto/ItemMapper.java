package ru.practicum.shareit.item.dto;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.util.List;
import java.util.stream.Collectors;

public class ItemMapper {

    public static ItemDto toItemDto(Item item) {
        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.isAvailable(),
                item.getRequest() != null ? item.getRequest().getId() : null
        );
    }

    public static Item toItem(ItemDto itemDto, User owner) {
        return Item.builder()
                .id(itemDto.getId())
                .name(itemDto.getName())
                .description(itemDto.getDescription())
                .available(itemDto.getAvailable())
                .owner(owner)
                .build();
    }

    public static ItemOwnerDto toItemOwnerDto(Item item, Booking lastBooking, Booking nextBooking,
                                              List<Comment> comments) {
        return new ItemOwnerDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.isAvailable(),
                item.getRequest() != null ? item.getRequest().getId() : null,
                lastBooking != null ? BookingMapper.toBookingShortDto(lastBooking) : null,
                nextBooking != null ? BookingMapper.toBookingShortDto(nextBooking) : null,
                comments.stream().map(CommentMapper::toCommentDto).collect(Collectors.toList())
        );
    }

    public static ItemDetailDto toItemDetailDto(Item item, Booking lastBooking, Booking nextBooking,
                                                List<Comment> comments) {
        return new ItemDetailDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.isAvailable(),
                item.getRequest() != null ? item.getRequest().getId() : null,
                lastBooking != null ? BookingMapper.toBookingShortDto(lastBooking) : null,
                nextBooking != null ? BookingMapper.toBookingShortDto(nextBooking) : null,
                comments.stream().map(CommentMapper::toCommentDto).collect(Collectors.toList())
        );
    }
}