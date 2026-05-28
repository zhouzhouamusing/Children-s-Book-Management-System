package com.kidsbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidsbook.entity.Reader;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReaderMapper extends BaseMapper<Reader> {

    @Select("SELECT COUNT(*) FROM reader WHERE status = 'normal'")
    int countActive();

    @Select("SELECT COUNT(*) FROM reader WHERE status = 'suspended'")
    int countSuspended();

    @Select("SELECT COUNT(*) FROM reader WHERE overdue_count > 0")
    int countWithOverdue();
}
