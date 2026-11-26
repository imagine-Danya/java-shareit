package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDetailDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemOwnerDto;

import java.util.List;

public interface ItemService {
    ItemDto create(Long userId, ItemDto itemDto);

    ItemDto update(Long userId, Long itemId, ItemDto itemDto);

    ItemDetailDto getById(Long userId, Long itemId);

    List<ItemOwnerDto> getAllByOwner(Long userId);

    List<ItemDto> search(Long userId, String text);

    CommentDto addComment(Long userId, Long itemId, CommentDto commentDto);
}