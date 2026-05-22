package com.comercialcloud.domain.shared;

import java.util.List;

public record PageResult<T>(List<T> content, long totalElements, int totalPages, int size, int number) {}
