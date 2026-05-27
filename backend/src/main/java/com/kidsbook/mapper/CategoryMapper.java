package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    @Select("SELECT COUNT(*) FROM book WHERE category = #{categoryName}")
    Integer countBooksByCategory(String categoryName);
}
