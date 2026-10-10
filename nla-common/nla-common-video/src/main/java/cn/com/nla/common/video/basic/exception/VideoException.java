package cn.com.nla.common.video.basic.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class VideoException extends Exception{

    public VideoException(String message){
        super(message);
    }

}
