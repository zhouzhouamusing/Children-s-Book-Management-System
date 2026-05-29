package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.ReadingProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReadingProgressMapper extends BaseMapper<ReadingProgress> {

    @Select("SELECT COALESCE(SUM(reading_minutes), 0) FROM reading_progress WHERE reader_id = #{readerId}")
    int sumReadingMinutesByReaderId(Long readerId);

    @Select("SELECT COUNT(*) FROM reading_progress WHERE reader_id = #{readerId} AND status = 'completed'")
    int countCompletedByReaderId(Long readerId);

    @Select("SELECT COUNT(*) FROM reading_progress WHERE reader_id = #{readerId}")
    int countByReaderId(Long readerId);
}
