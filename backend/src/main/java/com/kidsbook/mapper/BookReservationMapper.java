package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.BookReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BookReservationMapper extends BaseMapper<BookReservation> {
    @Select("SELECT COUNT(*) FROM book_reservation WHERE book_id = #{bookId} AND status IN ('pending', 'ready_for_pickup')")
    int countActiveByBookId(Long bookId);

    @Select("SELECT COUNT(*) FROM book_reservation WHERE reader_id = #{readerId} AND status IN ('pending', 'ready_for_pickup')")
    int countActiveByReaderId(Long readerId);
}
