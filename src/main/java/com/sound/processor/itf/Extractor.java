package com.sound.processor.itf;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import com.sound.processor.exception.AudioProcessException;
import com.sound.processor.model.AudioInfo;
import com.sound.processor.model.Wave;

public interface Extractor {

	
	/**
	 * Extract wave data from sound
	 * 
	 * @param sound
	 * @return
	 */
	public Wave extractWaveByUnit(File sound, Integer wavePointsPerS);

	public Wave extractWaveByTotal(InputStream sound, Integer extractWaveByTotal) throws AudioProcessException;
	
	public Wave extractWaveByTotal(URL url, Integer totalWavePoints) throws AudioProcessException;

	public AudioInfo extractInfo(File sound) throws AudioProcessException;
}
