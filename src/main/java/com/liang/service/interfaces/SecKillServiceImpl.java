package com.liang.service.interfaces;

import com.liang.dao.SeckillMapper;
import com.liang.dao.SuccessKilledMapper;
import com.liang.dao.cache.RedisDao;
import com.liang.dto.Exposer;
import com.liang.dto.SeckillExecution;
import com.liang.entity.Seckill;
import com.liang.entity.SuccessKilled;
import com.liang.enums.SeckillStatEnum;
import com.liang.exception.RepeatKillException;
import com.liang.exception.SecKillException;
import com.liang.exception.SeckillCloseException;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecKillServiceImpl implements SecKillService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 增加一个盐值，用于混淆
     */
    private final String salt = "thisIsASaltValue";

    @Autowired
    private SeckillMapper seckillMapper;

    @Autowired
    private SuccessKilledMapper successKilledMapper;

    @Autowired
    private RedisDao redisDao;

    @Override
    public List<Seckill> getSecKillList() {
        return seckillMapper.queryAll(0,4);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillMapper.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        //根据id去查询是否存在这个商品
        /*Seckill seckill = seckillMapper.queryById(seckillId);
        if(seckill == null){
            logger.warn("查询不到这个商品的记录");
            return new Exposer(false,seckillId);
        }*/
        Seckill seckill = redisDao.getSeckill(seckillId);
        if(seckill == null){
            //访问数据库读取数据
            seckill = seckillMapper.queryById(seckillId);
            if(seckill == null){
                return new Exposer(false,seckillId);
            }else{
                //放入redis
                redisDao.putSeckill(seckill);
            }
        }
        //判断是否还没到秒杀时间或者是过了秒杀时间
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date nowTime = new Date();
        if(nowTime.getTime() > startTime.getTime() && nowTime.getTime() < endTime.getTime()){
            String md5 = getMd5(seckillId);
            return new Exposer(true, md5, seckillId);
        }
        return new Exposer(false,seckillId,nowTime.getTime(),startTime.getTime(), endTime.getTime());
    }

    private String getMd5(long seckillId){
        String base = seckillId + "/" + salt;
        return DigestUtils.md5DigestAsHex(base.getBytes());
    }

    /**
     * 执行秒杀操作，失败我们抛出异常
     * @param seckillId 秒杀的商品Id
     * @param userPhone 手机号码
     * @param md5 md5加密值
     * @return 根据不同的结果返回不同的实体信息
     * @throws SecKillException
     * @throws RepeatKillException
     * @throws SecKillException
     */
    @Override
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SecKillException, RepeatKillException, SecKillException {

        if(md5 == null || !md5.equals(getMd5(seckillId))){
            logger.error("秒杀数据被篡改");
            throw new SecKillException("seckill data rewrite");
        }
        //执行秒杀业务逻辑
        Date nowTime = new Date();
        try{
            //记录购买行为
            int insertCount = successKilledMapper.insertSuccessKilled(seckillId,userPhone);
            if(insertCount <= 0){
                //重复秒杀
                throw new RepeatKillException("seckill repeated");
            }else{
                //减库存，热点商品的竞争
                int reduceNumber = seckillMapper.reduceNumber(seckillId,nowTime);
                if(reduceNumber <= 0){
                    logger.warn("没有更新数据库记录，说明秒杀结束");
                    throw new SeckillCloseException("seckill is closed");
                }else{
                    //秒杀成功了，返回那条插入成功秒杀的信息，进行commit
                    SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(seckillId,userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS,successKilled);
                }
            }
        }catch(SeckillCloseException | RepeatKillException el){
            throw el;
        }
    }

    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {

        if(md5 == null || !md5.equals(getMd5(seckillId))){
            return new SeckillExecution(seckillId,SeckillStatEnum.DATE_REWRITE);
        }
        Date killTime = new Date();
        Map<String,Object> map = new HashMap<>();
        map.put("seckillId",seckillId);
        map.put("phone",userPhone);
        map.put("killTime",killTime);
        map.put("result",null);

        //执行存储过程，result被复制
        try{
            seckillMapper.killByProcedure(map);
            //获取result
            int result = MapUtils.getInteger(map,"result",-2);
            if(result == 1){
                SuccessKilled successKilled = successKilledMapper.queryByIdWithSeckill(seckillId,userPhone);
                return new SeckillExecution(seckillId,SeckillStatEnum.SUCCESS,successKilled);
            }else{
                return new SeckillExecution(seckillId,SeckillStatEnum.stateOf(result));
            }
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            return new SeckillExecution(seckillId,SeckillStatEnum.INNER_ERROR);
        }
    }
}