package cn.garymb.ygoserver.ygo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import cn.garymb.ygoserver.util.TextUtils;

public class BanlistManager {
	private static List<Banlist> sBanlists;

	public static void create(String fileName) {
		sBanlists = new ArrayList<Banlist>();
		Banlist current = null;
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(new File(fileName), "r");
			String line = null;
			do {
				line = raf.readLine();
				if (TextUtils.isEmpty(line)) {
					continue;
				}
				if (line.startsWith("#")) {
					continue;
				}
				if (line.startsWith("!")) {
					current = new Banlist();
					sBanlists.add(current);
				}
				if (!line.contains(" ")){
					continue;
				}
				if (current == null) {
					continue;
				}
				String[] data = line.split(" ");
				int id = Integer.parseInt(data[0]);
				int count = Integer.parseInt(data[1]);
				current.add(id, count);
			} while (line != null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException e) {
					//just incase
				}
			}
		}
	}
	public int getIndex(int hash) {
		if (sBanlists == null) {
			return 0;
		}
		for (Banlist l : sBanlists) {
			if (l.hashCode() == hash) {
				return sBanlists.indexOf(l);
			}
		}
		return 0;
	}
	public static int size() {
		return sBanlists == null ? 0 :sBanlists.size();
	}
	
	public static Banlist get(int index) {
		return sBanlists.get(index);
	}
}
