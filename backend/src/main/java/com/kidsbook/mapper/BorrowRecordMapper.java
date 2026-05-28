package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    @Select("SELECT COUNT(*) FROM borrow_record WHERE reader_id = #{readerId} AND status = 'overdue'")
    int countOverdueByReaderId(Long readerId);

    @Select("SELECT COUNT(*) FROM borrow_record WHERE reader_id = #{readerId}")
    int countByReaderId(Long readerId);
}
