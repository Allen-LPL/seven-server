package cn.iocoder.yudao.module.system.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MilvusClientConfig {

  @Value("${milvus.host}")
  private String milvusHost ;
  @Value("${milvus.port}")
  private int milvusPort;

  @Bean("imageMilvusClient")
  public MilvusServiceClient imageMilvusClient(){
    log.info("milvusServiceClient init , milvusHost:{} , milvusPort:{}" , milvusHost , milvusPort);
    return  new MilvusServiceClient(
        ConnectParam.newBuilder()
            .withHost(milvusHost)
            .withPort(milvusPort)
            .build());
  }

}
