package com.lionico.devinspect;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Environment;
import android.os.StatFs;

public class FileExplorerFragment extends Fragment {

    private TextView tvStorageInfo, tvRootInfo, tvSdCardInfo;
    private Button btnRefreshStorage, btnListFiles, btnCheckPermissions;

    public FileExplorerFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_explorer, container, false);

        initializeViews(view);
        setupButtons();
        updateStorageInfo();

        return view;
    }

    private void initializeViews(View view) {
        tvStorageInfo = (TextView) view.findViewById(R.id.tvStorageInfo);
        tvRootInfo = (TextView) view.findViewById(R.id.tvRootInfo);
        tvSdCardInfo = (TextView) view.findViewById(R.id.tvSdCardInfo);
        btnRefreshStorage = (Button) view.findViewById(R.id.btnRefreshStorage);
        btnListFiles = (Button) view.findViewById(R.id.btnListFiles);
        btnCheckPermissions = (Button) view.findViewById(R.id.btnCheckPermissions);
    }

    private void setupButtons() {
        btnRefreshStorage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateStorageInfo();
                }
            });

        btnListFiles.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listSampleFiles();
                }
            });

        btnCheckPermissions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkStorageAccess();
                }
            });
    }

    private void updateStorageInfo() {
        // Internal Storage
        String internalInfo = getStorageInfo(Environment.getDataDirectory());
        tvStorageInfo.setText(internalInfo);

        // Root Directory
        String rootInfo = getDirectoryInfo(Environment.getRootDirectory());
        tvRootInfo.setText(rootInfo);

        // External Storage (SD Card)
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File externalDir = Environment.getExternalStorageDirectory();
            String sdInfo = getStorageInfo(externalDir);
            tvSdCardInfo.setText("Available\n" + sdInfo);
        } else {
            tvSdCardInfo.setText("Not Available\n" + state);
        }
    }

    private String getStorageInfo(File path) {
        try {
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();

            long totalSize = totalBlocks * blockSize;
            long availableSize = availableBlocks * blockSize;
            long usedSize = totalSize - availableSize;

            double totalGB = totalSize / (1024.0 * 1024.0 * 1024.0);
            double usedGB = usedSize / (1024.0 * 1024.0 * 1024.0);
            double availableGB = availableSize / (1024.0 * 1024.0 * 1024.0);

            return String.format(Locale.US, "Total: %.2f GB\nUsed: %.2f GB\nFree: %.2f GB", 
                                 totalGB, usedGB, availableGB);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String getDirectoryInfo(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            int fileCount = files != null ? files.length : 0;
            long lastModified = dir.lastModified();
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .format(new Date(lastModified));

            return "Files: " + fileCount + "\nModified: " + date;
        }
        return "Not accessible";
    }

    private void listSampleFiles() {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);

            if (downloadsDir.exists() && downloadsDir.isDirectory()) {
                File[] files = downloadsDir.listFiles();
                int count = files != null ? Math.min(files.length, 5) : 0;

                String message = "Downloads (" + count + " files shown):\n";
                for (int i = 0; i < count && i < 5; i++) {
                    message += "• " + files[i].getName() + "\n";
                }

                if (getActivity() != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            } else {
                showToast("Downloads directory not found");
            }
        } catch (SecurityException e) {
            showToast("Permission denied for downloads");
        } catch (Exception e) {
            showToast("Error: " + e.getMessage());
        }
    }

    private void checkStorageAccess() {
        String state = Environment.getExternalStorageState();
        String message = "Storage State: " + state + "\n";

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            message += "Read/Write access: YES";
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            message += "Read/Write access: READ ONLY";
        } else {
            message += "Read/Write access: NO";
        }

        showToast(message);
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
    }
}
