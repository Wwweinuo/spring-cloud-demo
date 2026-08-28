package com.wwweinuo.cloudmall.api.user.dto;

/**
 * user-service 对外返回的用户数据，不暴露数据库实体。
 */
public record UserDTO(
        Long id,
        String username,
        String status
) {
}
