package com.knowledgebase.sopviewer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

/**
 * Shows a BottomSheetDialog with full document details (title, badges, description,
 * version) and a Download button that triggers the actual file download.
 */
public class DownloadSheet {

    /**
     * Show the preview sheet for a document card.
     *
     * @param context     Activity or application context
     * @param title       Document title
     * @param description Document description
     * @param category    Document category
     * @param fileType    File type string (e.g. "pdf", "doc")
     * @param version     Version string
     * @param date        Date / last-updated string
     * @param status      Status string: "approved", "rejected", "pending", or ""
     * @param fileUrl     Raw file URL (will be resolved inside DownloadHelper)
     */
    public static void show(Context context,
            String title, String description, String category,
            String fileType, String version, String date, String status,
            String fileUrl) {

        BottomSheetDialog sheet = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.sheet_download_preview, null);
        sheet.setContentView(view);

        // Title
        TextView tvTitle = view.findViewById(R.id.sheetDocTitle);
        tvTitle.setText(title != null ? title : "");

        // Category badge
        TextView tvCategory = view.findViewById(R.id.sheetCategory);
        if (category != null && !category.isEmpty()) {
            tvCategory.setText(category);
            tvCategory.setVisibility(View.VISIBLE);
        }

        // File type badge
        TextView tvFileType = view.findViewById(R.id.sheetFileType);
        if (fileType != null && !fileType.isEmpty()) {
            tvFileType.setText(fileType.toUpperCase());
            tvFileType.setVisibility(View.VISIBLE);
        }

        // Status badge
        TextView tvStatus = view.findViewById(R.id.sheetStatus);
        applyStatusBadge(tvStatus, status);

        // Description
        TextView tvDesc = view.findViewById(R.id.sheetDescription);
        tvDesc.setText(description != null && !description.isEmpty()
                ? description : "No description available.");

        // Version
        TextView tvVersion = view.findViewById(R.id.sheetVersion);
        String versionText = "Version: " + (version != null && !version.isEmpty() ? version : "1.0.0");
        if (date != null && !date.isEmpty()) versionText += "\n" + date;
        tvVersion.setText(versionText);

        // Close button
        view.findViewById(R.id.sheetBtnClose).setOnClickListener(v -> sheet.dismiss());

        // Download button
        MaterialButton btnDownload = view.findViewById(R.id.sheetBtnDownload);
        btnDownload.setOnClickListener(v -> {
            sheet.dismiss();
            DownloadHelper.download(context, fileUrl, title, fileType, description);
        });

        sheet.show();
    }

    /** Convenience overload for RecentDoc objects. */
    public static void show(Context context, RecentDoc doc) {
        show(context,
                doc.getTitle(),
                doc.getDescription(),
                doc.getCategory(),
                doc.getFileType(),
                doc.getVersion(),
                doc.getDate(),
                doc.getStatus(),
                doc.getFileUrl());
    }

    private static void applyStatusBadge(TextView badge, String status) {
        if (status == null || status.isEmpty()) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setVisibility(View.VISIBLE);
        switch (status) {
            case "approved":
                badge.setText("Approved");
                badge.setTextColor(Color.parseColor("#16A34A"));
                badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#F0FDF4")));
                break;
            case "rejected":
                badge.setText("Rejected");
                badge.setTextColor(Color.parseColor("#DC2626"));
                badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2")));
                break;
            default:
                badge.setText("Pending Approval");
                badge.setTextColor(Color.parseColor("#D97706"));
                badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFBEB")));
                break;
        }
    }
}
