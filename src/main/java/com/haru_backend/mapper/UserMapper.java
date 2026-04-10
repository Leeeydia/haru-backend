package com.haru_backend.mapper;

import com.haru_backend.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByEmail(@Param("email") String email);

    User findById(@Param("id") Long id);

    void insertUser(User user);
}
