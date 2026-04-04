package springai.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import springai.entity.Session;

@Mapper
public interface SessionMapper extends BaseMapper<Session>{
    
}
