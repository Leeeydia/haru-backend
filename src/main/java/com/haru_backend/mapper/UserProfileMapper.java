package com.haru_backend.mapper;

import com.haru_backend.domain.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper {

    void insertProfile(UserProfile profile);

    UserProfile findByUserId(@Param("userId") Long userId);

    void updateProfile(UserProfile profile);
}
