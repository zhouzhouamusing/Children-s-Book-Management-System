package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
    @Select("SELECT category, COUNT(*) as count FROM book GROUP BY category")
    List<Map<String, Object>> countByCategory();

    @Select("SELECT COUNT(*) FROM book")
    Integer totalCount();

    @Select("SELECT COALESCE(SUM(stock), 0) FROM book")
    Integer totalStock();
}
