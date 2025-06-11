package com.example.first;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GallaryAdapter extends RecyclerView.Adapter<GallaryAdapter.GallaryViewHolder> {

    private final List<GallaryItem> gallaryItemList;
    private final OnGallaryItemClickListener clickListener;
    private final OnGallaryItemLongClickListener longClickListener;
    private OnSelectionModeChangeListener selectionModeChangeListener;
    private boolean isSelectionMode = false;
    private final Map<String, Boolean> selectedItems = new HashMap<>(); // Use path as key for uniqueness

    public interface OnGallaryItemClickListener {
        void onFileClick(GallaryItem gallaryItem);
    }

    public interface OnGallaryItemLongClickListener {
        void onFileLongClick(GallaryItem gallaryItem);
    }

    public interface OnSelectionModeChangeListener {
        void onSelectionModeChanged(boolean isSelectionMode, int selectedCount);
    }

    public GallaryAdapter(List<GallaryItem> gallaryItemList,
                          OnGallaryItemClickListener clickListener,
                          OnGallaryItemLongClickListener longClickListener) {
        this.gallaryItemList = gallaryItemList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setSelectionModeChangeListener(OnSelectionModeChangeListener listener) {
        this.selectionModeChangeListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedItems.clear();
        }
        notifyDataSetChanged();
        updateSelectionModeCallback();
    }

    public void setItemSelected(GallaryItem item, boolean selected) {
        if (selected) {
            selectedItems.put(item.getPath(), true);
        } else {
            selectedItems.remove(item.getPath());
        }
        int position = gallaryItemList.indexOf(item);
        if (position != -1) {
            notifyItemChanged(position);
        }
        updateSelectionModeCallback();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void selectAll() {
        selectedItems.clear();
        for (GallaryItem item : gallaryItemList) {
            selectedItems.put(item.getPath(), true);
        }
        notifyDataSetChanged();
        updateSelectionModeCallback();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
        updateSelectionModeCallback();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public boolean isAllItemsSelected() {
        return selectedItems.size() == gallaryItemList.size() && !gallaryItemList.isEmpty();
    }

    public boolean isInSelectionMode() {
        return isSelectionMode;
    }

    // NEW METHOD: Get list of selected items for sending
    public List<GallaryItem> getSelectedItems() {
        List<GallaryItem> selected = new ArrayList<>();
        for (GallaryItem item : gallaryItemList) {
            if (selectedItems.containsKey(item.getPath())) {
                selected.add(item);
            }
        }
        return selected;
    }

    // Check if an item is selected
    private boolean isItemSelected(GallaryItem item) {
        return selectedItems.containsKey(item.getPath());
    }

    private void updateSelectionModeCallback() {
        if (selectionModeChangeListener != null) {
            selectionModeChangeListener.onSelectionModeChanged(isSelectionMode, getSelectedItemCount());
        }
    }

    @NonNull
    @Override
    public GallaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallary, parent, false);
        return new GallaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GallaryViewHolder holder, int position) {
        GallaryItem gallaryItem = gallaryItemList.get(position);
        holder.bind(gallaryItem);
    }

    @Override
    public int getItemCount() {
        return gallaryItemList.size();
    }

    class GallaryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView fileIcon;
        private final TextView fileName;
        private final TextView fileSize;
        private final TextView fileType;
        private final CheckBox selectionCheckBox;
        private final View itemContainer;

        public GallaryViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileSize = itemView.findViewById(R.id.fileSize);
            fileType = itemView.findViewById(R.id.fileType);
            selectionCheckBox = itemView.findViewById(R.id.selectionCheckBox);
            itemContainer = itemView.findViewById(R.id.itemContainer);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    GallaryItem item = gallaryItemList.get(position);
                    if (isSelectionMode) {
                        // Toggle selection in selection mode
                        boolean isSelected = isItemSelected(item);
                        setItemSelected(item, !isSelected);
                    } else if (clickListener != null) {
                        // Normal click behavior
                        clickListener.onFileClick(item);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    if (!isSelectionMode) {
                        // Start selection mode on long click
                        setSelectionMode(true);
                        GallaryItem item = gallaryItemList.get(position);
                        setItemSelected(item, true);
                    }
                    if (longClickListener != null) {
                        longClickListener.onFileLongClick(gallaryItemList.get(position));
                    }
                    return true;
                }
                return false;
            });

            // Handle checkbox clicks
            if (selectionCheckBox != null) {
                selectionCheckBox.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && isSelectionMode) {
                        GallaryItem item = gallaryItemList.get(position);
                        boolean isSelected = isItemSelected(item);
                        setItemSelected(item, !isSelected);
                    }
                });
            }
        }

        public void bind(GallaryItem gallaryItem) {
            fileName.setText(gallaryItem.getName());
            fileSize.setText(formatFileSize(gallaryItem.getSize()));
            fileType.setText(gallaryItem.getType());

            // Handle selection mode UI
            if (selectionCheckBox != null) {
                if (isSelectionMode) {
                    selectionCheckBox.setVisibility(View.VISIBLE);
                    boolean isSelected = isItemSelected(gallaryItem);

                    // Set checkbox state - this will show check/uncheck automatically
                    selectionCheckBox.setChecked(isSelected);

                    // Update item background for selection feedback
                    if (itemContainer != null) {
                        if (isSelected) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                itemContainer.setBackgroundColor(
                                        itemView.getContext().getResources().getColor(android.R.color.holo_blue_light, null)
                                );
                            } else {
                                itemContainer.setBackgroundColor(
                                        itemView.getContext().getResources().getColor(android.R.color.holo_blue_light)
                                );
                            }
                            itemContainer.setAlpha(0.7f);
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                itemContainer.setBackgroundColor(
                                        itemView.getContext().getResources().getColor(android.R.color.transparent, null)
                                );
                            } else {
                                itemContainer.setBackgroundColor(
                                        itemView.getContext().getResources().getColor(android.R.color.transparent)
                                );
                            }
                            itemContainer.setAlpha(1.0f);
                        }
                    }
                } else {
                    selectionCheckBox.setVisibility(View.GONE);
                    if (itemContainer != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            itemContainer.setBackgroundColor(
                                    itemView.getContext().getResources().getColor(android.R.color.transparent, null)
                            );
                        } else {
                            itemContainer.setBackgroundColor(
                                    itemView.getContext().getResources().getColor(android.R.color.transparent)
                            );
                        }
                        itemContainer.setAlpha(1.0f);
                    }
                }
            }

            // Load image or set appropriate icon based on file type
            loadFileIcon(gallaryItem);
        }

        private void loadFileIcon(GallaryItem gallaryItem) {
            String fileType = gallaryItem.getType().toLowerCase();

            // Check if it's an image file
            if (isImageFile(fileType)) {
                // Load the actual image file as thumbnail
                loadImageThumbnail(gallaryItem.getPath());
            } else {
                // Set appropriate icon for non-image files
                setGenericFileIcon(fileType);
            }
        }

        private boolean isImageFile(String fileType) {
            return fileType.equals("jpg") || fileType.equals("jpeg") ||
                    fileType.equals("png") || fileType.equals("gif") ||
                    fileType.equals("bmp") || fileType.equals("webp");
        }

        private void loadImageThumbnail(String imagePath) {
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery);

            Glide.with(itemView.getContext())
                    .load(imagePath)
                    .apply(options)
                    .into(fileIcon);
        }

        private void setGenericFileIcon(String fileType) {
            int iconResource;

            switch (fileType) {
                case "pdf":
                case "txt":
                case "doc":
                case "docx":
                    iconResource = android.R.drawable.ic_menu_edit;
                    break;
                case "mp4":
                case "avi":
                case "mkv":
                case "mov":
                    iconResource = android.R.drawable.ic_media_play;
                    break;
                case "mp3":
                case "wav":
                case "aac":
                    iconResource = android.R.drawable.ic_lock_silent_mode_off;
                    break;
                case "zip":
                case "rar":
                case "7z":
                    iconResource = android.R.drawable.ic_menu_save;
                    break;
                case "apk":
                    iconResource = android.R.drawable.ic_menu_preferences;
                    break;
                default:
                    iconResource = android.R.drawable.ic_menu_info_details;
                    break;
            }

            fileIcon.setImageResource(iconResource);
        }

        @SuppressLint("DefaultLocale")
        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            else if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            else return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}