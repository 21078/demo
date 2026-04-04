package springai.service.iml;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import springai.entity.Session;
import springai.mapper.SessionMapper;
import springai.service.SessionService;
@Service
public class SeesionServiceImp extends ServiceImpl<SessionMapper,Session> implements SessionService{
    
}
