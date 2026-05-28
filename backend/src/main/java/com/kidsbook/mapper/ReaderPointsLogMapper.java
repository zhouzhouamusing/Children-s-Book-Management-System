package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.ReaderPointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReaderPointsLogMapper extends BaseMapper<ReaderPointsLog> {

    @Select("SELECT COALESCE(SUM(points), 0) FROM reader_points_log WHERE reader_id = #{readerId}")
    int sumPointsByReaderId(Long readerId);

    @Select("SELECT type, COALESCE(SUM(points), 0) as total FROM reader_points_log WHERE reader_id = #{readerId} GROUP BY type")
    List<Map<String, Object>> sumPointsByType(Long readerId);
}
