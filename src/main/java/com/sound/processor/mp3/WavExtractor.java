package com.sound.processor.mp3;

import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.MultimediaInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FileUtils;

import com.sound.processor.exception.AudioProcessException;
import com.sound.processor.itf.Extractor;
import com.sound.processor.model.AudioInfo;
import com.sound.processor.model.Wave;

public class WavExtractor implements Extractor {

  public static final int DEFULAT_WAVE_NUM = 1300;
  public static final float Signed16BitRange = 32768;
  public static final float Signed8BitBRange = 128;
  public static final float Unsigned8BitBRange = 256;

  public Wave extractWaveByUnit(File sound, Integer wavePointsPerS) {
    if (null == sound || !sound.exists()) {
      throw new RuntimeException("File not found.");
    }

    wavePointsPerS = (null == wavePointsPerS) ? 15 : wavePointsPerS;
    AudioInputStream sourceStream = null;
    try {
      sourceStream = AudioSystem.getAudioInputStream(sound);
      int numChannels = sourceStream.getFormat().getChannels();
      int sampleInterval =
          (int) (sourceStream.getFormat().getSampleRate() / (wavePointsPerS * numChannels));
      int frameLength = (int) sourceStream.getFrameLength();
      int frameSize = (int) sourceStream.getFormat().getFrameSize();
      int sampleLength =
          (int) ((sourceStream.getFormat().getSampleRate() / sourceStream.getFormat()
              .getFrameRate()) * frameLength);

      byte[] eightBitByteArray = new byte[frameLength * frameSize];
      sourceStream.read(eightBitByteArray);

      float[][] waveData =
          extractWaveData(sampleLength / sampleInterval, sourceStream, numChannels, sampleLength,
              sampleInterval, eightBitByteArray);

      return new Wave(waveData);
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Wave extractWaveByTotal(InputStream sound, Integer totalWavePoints)
      throws AudioProcessException {
    if (null == sound) {
      throw new RuntimeException("Stream is empty");
    }
    totalWavePoints = (null == totalWavePoints) ? DEFULAT_WAVE_NUM : totalWavePoints;
    AudioInputStream sourceStream = null;
    try {
      sourceStream = AudioSystem.getAudioInputStream(sound);
      int numChannels = sourceStream.getFormat().getChannels();
      int frameLength = (int) sourceStream.getFrameLength();
      int frameSize = (int) sourceStream.getFormat().getFrameSize();
      int totalSampleCount =
          (int) ((sourceStream.getFormat().getSampleRate() / sourceStream.getFormat()
              .getFrameRate()) * frameLength);
      int sampleInterval = totalSampleCount / (totalWavePoints);

      byte[] eightBitByteArray = new byte[frameLength * frameSize];
      sourceStream.read(eightBitByteArray);

      float[][] waveData =
          extractWaveData(totalWavePoints, sourceStream, numChannels, totalSampleCount,
              sampleInterval, eightBitByteArray);

      float[][] waves = null;

      if (waveData.length == 1) {
        waves = waveData;
      } else {
        waves = new float[1][];
        waves[0] = waveData[1];
      }

      return new Wave(waves);
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
      throw new AudioProcessException(e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new AudioProcessException(e);
    }
  }

  public Wave extractWaveByTotal(URL url, Integer totalWavePoints) throws AudioProcessException {
    if (null == url) {
      throw new RuntimeException("URL is empty");
    }
    totalWavePoints = (null == totalWavePoints) ? DEFULAT_WAVE_NUM : totalWavePoints;
    AudioInputStream sourceStream = null;

    File temp = null;

    try {
      temp = File.createTempFile("temp", "");
      FileUtils.copyURLToFile(url, temp);

      sourceStream = AudioSystem.getAudioInputStream(temp);
      int numChannels = sourceStream.getFormat().getChannels();
      int frameLength = (int) sourceStream.getFrameLength();
      int frameSize = (int) sourceStream.getFormat().getFrameSize();
      int totalSampleCount =
          (int) ((sourceStream.getFormat().getSampleRate() / sourceStream.getFormat()
              .getFrameRate()) * frameLength);
      int sampleInterval = totalSampleCount / (totalWavePoints);

      byte[] eightBitByteArray = new byte[frameLength * frameSize];
      sourceStream.read(eightBitByteArray);

      float[][] waveData =
          extractWaveData(totalWavePoints, sourceStream, numChannels, totalSampleCount,
              sampleInterval, eightBitByteArray);

      float[][] waves = null;

      if (waveData.length == 1) {
        waves = waveData;
      } else {
        // 双声道音频提前右声道(人声)
        waves = new float[1][];
        waves[0] = waveData[1];
      }

      return new Wave(waves);
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
      throw new AudioProcessException(e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new AudioProcessException(e);
    } finally {
      if (null != temp) {
        FileUtils.deleteQuietly(temp);
      }
    }
  }

  private float[][] extractWaveData(Integer totalWavePoints, AudioInputStream sourceStream,
      int numChannels, int sampleLength, int sampleInterval, byte[] eightBitByteArray) {
    float[][] waveData = new float[numChannels][totalWavePoints];
    int wavePointIndex = 0;
    float range = 0;

    for (int sampleIndex = 0; sampleIndex < sampleLength;) {
      if (wavePointIndex < totalWavePoints && sampleIndex % sampleInterval == 0) {
        
        for (int channel = 0; channel < numChannels; channel++) {
          switch (sourceStream.getFormat().getSampleSizeInBits()) {
            case 8:
              if (sourceStream.getFormat().getEncoding().toString().toLowerCase()
                  .startsWith("pcm_sign")) {
                if ((eightBitByteArray[sampleIndex]) > range) {
                  range = (eightBitByteArray[sampleIndex]);
                }
                waveData[channel][wavePointIndex] = eightBitByteArray[sampleIndex + channel];
              } else {
                if ((eightBitByteArray[sampleIndex] & 0xFF) > range) {
                  range = (eightBitByteArray[sampleIndex] & 0xFF);
                }
                waveData[channel][wavePointIndex] = (eightBitByteArray[sampleIndex + channel] & 0xFF);
              }
              break;
            case 16:
              int low = 0, high = 0;
              if (sourceStream.getFormat().isBigEndian()) {
                high = (int) eightBitByteArray[sampleIndex*2*numChannels + 2*channel];
                low = (int) eightBitByteArray[sampleIndex*2*numChannels+ 2*channel +1];
              } else {
                low = (int) eightBitByteArray[sampleIndex*2*numChannels + 2*channel];
                high = (int) eightBitByteArray[sampleIndex*2*numChannels + 2*channel +1];
              }
              float temp = (float) Math.sqrt(Math.sqrt(Math.abs(get16BitSample(high, low))));
              if (temp > range) {
                range = temp;
              }
              waveData[channel][wavePointIndex] = temp;
              break;
            default:
              break;
          }
        }
        wavePointIndex++;
      } 
      sampleIndex++;
    }
    
    for (int i = 0; i < waveData.length; i++) {
      for (int j = 0; j < waveData[i].length; j++) {
        BigDecimal waveRange = new BigDecimal(waveData[i][j] / range);
        waveData[i][j] = waveRange.setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
        waveData[i][j] = (waveData[i][j]>1)?1: waveData[i][j];
      }
    }

    return waveData;
  }

  /**
   * 
   * @param high
   * @param low
   * @return
   */
  private float get16BitSample(int high, int low) {
    return (high << 8) | (low & 0x00ff);
  }

  public AudioInfo extractInfo(File sound) throws AudioProcessException {
    if (null == sound || !sound.exists()) {
      throw new RuntimeException("File not found.");
    }



    Encoder encoder = null;
    try {
      encoder = new Encoder();
      MultimediaInfo medieInfo = encoder.getInfo(sound);
      it.sauronsoftware.jave.AudioInfo sourceInfo = medieInfo.getAudio();

      return new AudioInfo(sourceInfo.getBitRate(), sourceInfo.getChannels(),
          sourceInfo.getSamplingRate(), medieInfo.getDuration());
    } catch (Exception e) {
      e.printStackTrace();
      throw new AudioProcessException(e);
    }
  }

  public static void main(String[] args) throws Exception {
    WavExtractor wavExtractor = new WavExtractor();
//   System.out.println(wavExtractor.extractInfo(new File("C:\\Users\\Public\\Music\\Sample Music\\K歌之王.mp3")));
//   System.out.println(wavExtractor.extractInfo(new File("C:\\server\\apache-tomcat-7.0.40\\temp\\test.wav")));
//    wavExtractor.extractWaveByTotal(new BufferedInputStream(new FileInputStream(new File("C:\\server\\apache-tomcat-7.0.40\\temp\\temp1063285017793354236.wav"))), null);      
   System.out.println();
    wavExtractor.extractWaveByTotal(new BufferedInputStream(new FileInputStream(new File("C:\\server\\apache-tomcat-7.0.40\\temp\\test.wav"))), null);      
    
//    wavExtractor.extractWaveByTotal(new URL("http://www-sigproc.eng.cam.ac.uk/~sjg/computer_music_transcription/sounds/flute.wav"), null);
  }

}
