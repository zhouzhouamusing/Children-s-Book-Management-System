package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.BookReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface BookReviewMapper extends BaseMapper<BookReview> {

    @Select("SELECT IFNULL(AVG(rating), 0) FROM book_review WHERE book_id = #{bookId} AND status = 'approved'")
    BigDecimal avgRatingByBookId(Long bookId);

    @Select("SELECT COUNT(*) FROM book_review WHERE book_id = #{bookId} AND status = 'approved'")
    Integer countApprovedByBookId(Long bookId);
}
