package com.sound.processor.mp3;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;

import java.io.File;

import com.sound.processor.base.BaseConverter;
import com.sound.processor.exception.AudioProcessException;

public class WavConverter extends BaseConverter{

	@Override
	public File convert(File source) throws AudioProcessException {
		if (null == source || !source.exists())
		{
			throw new RuntimeException("source file doesn't exist.");
		}

		Encoder encoder = null;
		try 
		{
			encoder = new Encoder();
//			AudioInfo sourceInfo = encoder.getInfo(source).getAudio();

			AudioAttributes audio = new AudioAttributes();
			audio.setCodec("pcm_u8");
			audio.setChannels(1);
			EncodingAttributes attrs = new EncodingAttributes();
			attrs.setFormat("wav");
			attrs.setAudioAttributes(audio);
			File target = new File("temp" + System.currentTimeMillis() + ".wav");
			encoder.encode(source, target, attrs);

			return target;
		} catch (InputFormatException e) {
			e.printStackTrace();
			throw new AudioProcessException(e);
		} catch (EncoderException e) {
			e.printStackTrace();
			throw new AudioProcessException(e);
		}
	}

	public static void main(String [] args) throws Exception
	{
		Encoder encoder = new Encoder();
		File source = new File("C:\\Users\\Public\\Music\\Sample Music\\K歌之王.mp3");
		File target = new File("C:\\server\\apache-tomcat-7.0.40\\temp\\test.wav");

		AudioAttributes audio = new AudioAttributes();
		audio.setCodec("pcm_s16le");
		audio.setChannels(2);
		audio.setBitRate(1411);
		EncodingAttributes attrs = new EncodingAttributes();
		attrs.setFormat("wav");
		attrs.setAudioAttributes(audio);
		encoder.encode(source, target, attrs);

		WavExtractor me = new WavExtractor();
		System.out.print(me.extractInfo(target));
	}
}
