package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.ReaderMonthlyStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReaderMonthlyStatsMapper extends BaseMapper<ReaderMonthlyStats> {

    @Select("SELECT * FROM reader_monthly_stats WHERE reader_id = #{readerId} ORDER BY year_month DESC LIMIT 12")
    List<ReaderMonthlyStats> getLast12Months(Long readerId);
}
