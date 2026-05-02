package com.streama.entity.config;//package com.streama.config;
//
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.core.TopicExchange;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.context.annotation.Bean;
//
//@
//public class RabbitConfig {
//
//    // 交换机名称
//    public static final String AUDIT_EXCHANGE = "audit.exchange";
//
//    // 队列名称
//    public static final String VISUAL_QUEUE = "visual.queue";
//    public static final String AUDIO_QUEUE = "audio.queue";
//
//    // 路由键
//    public static final String VISUAL_ROUTING_KEY = "audit.visual";
//    public static final String AUDIO_ROUTING_KEY = "audit.audio";
//
//    /**
//     * 创建主题交换机
//     */
//    @Bean
//    public TopicExchange auditExchange() {
//        // 主题交换机，支持通配符路由键
//        return new TopicExchange(AUDIT_EXCHANGE, true, false);
//    }
//
//    /**
//     * 创建视觉任务队列
//     */
//    @Bean
//    public Queue visualQueue() {
//        // 持久化队列
//        return new Queue(VISUAL_QUEUE, true);
//    }
//
//    /**
//     * 创建音频任务队列
//     */
//    @Bean
//    public Queue audioQueue() {
//        // 持久化队列
//        return new Queue(AUDIO_QUEUE, true);
//    }
//
//    /**
//     * 绑定视觉队列到交换机
//     */
//    @Bean
//    public Binding visualBinding(Queue visualQueue, TopicExchange auditExchange) {
//        return BindingBuilder.bind(visualQueue).to(auditExchange).with(VISUAL_ROUTING_KEY);
//    }
//
//    /**
//     * 绑定音频队列到交换机
//     */
//    @Bean
//    public Binding audioBinding(Queue audioQueue, TopicExchange auditExchange) {
//        return BindingBuilder.bind(audioQueue).to(auditExchange).with(AUDIO_ROUTING_KEY);
//    }
//
//    /**
//     * 配置Jackson2JsonMessageConverter
//     * 用于将Java对象转换为JSON格式发送到RabbitMQ
//     */
//    @Bean
//    public Jackson2JsonMessageConverter jsonMessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//}
