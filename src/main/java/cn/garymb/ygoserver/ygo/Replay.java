package cn.garymb.ygoserver.ygo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import SevenZip.Compression.LZMA.Encoder;
import cn.garymb.ygoserver.conf.Configurator;
import cn.garymb.ygoserver.server.YGOCoreMain;

public class Replay {
	public static class ReplayHeader {
        public int id;
        public int version;
        public int flag;
        public int seed;
        public int dataSize;
        public int hash;
        public byte[] props;
	}

	public static final int FLAG_COMPRESSED = 0x1;
	public static final int FLAG_TAG = 0x2;
	
	public static final int MAX_REPLAY_SIZE = 0x20000;
	private volatile boolean disabled;
	private ReplayHeader header;
	
	private ByteBuffer mData;
	
	public Replay(int seed, boolean tag) {
		header = new ReplayHeader();
		header.id = 0x31707279;
		header.version = Integer.parseInt(YGOCoreMain.getConfigurator().getClientVersion(), 16);
		header.flag = tag ? FLAG_TAG : 0;
		header.seed = seed;
		mData = ByteBuffer.allocate(MAX_REPLAY_SIZE);
	}
	
	public synchronized void write(byte[] data) {
		mData.put(data);
	}
	
	public synchronized void end() {
		if (isDisabled()) {
			return;
		}
		header.dataSize = mData.position();
		header.flag |= FLAG_COMPRESSED;
		Encoder lzma = new Encoder();
		ByteArrayOutputStream bos = null;
		InputStream is = null;
		try {
			bos = new ByteArrayOutputStream();
			is = new ByteArrayInputStream(mData.array());
			
			lzma.WriteCoderProperties(bos);
			bos.flush();
			header.props = bos.toByteArray();
			bos.reset();
			
			lzma.Code(is, bos, header.dataSize, -1, null);
			bos.flush();
			
			mData.clear();
			mData.putInt(header.id);
			mData.putInt(header.version);
			mData.putInt(header.flag);
			mData.putInt(header.seed);
			mData.putInt(header.dataSize);
			mData.putInt(header.hash);
			mData.put(header.props);
			mData.put(bos.toByteArray());
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					//just in case
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					//just in case
				}
			}
		}
		
	}
	
	public final byte[] getSavedData() {
		return mData.array();
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
}
