<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Document extends Model
{
    use HasFactory;

    protected $fillable = ['title', 'description', 'category_id', 'created_by', 'is_active','status',
        'reviewed_by',
        'reviewed_at'];

    protected $casts = [
        'is_active' => 'boolean',
        'status' => 'integer',
        'reviewed_at' => 'datetime',
    ];

    public function category()
    {
        return $this->belongsTo(Category::class);
    }

    public function creator()
    {
        return $this->belongsTo(User::class, 'created_by');
    }

    public function versions()
    {
        // Latest version first — Android's getVersions().get(0) always returns the newest
        return $this->hasMany(DocumentVersion::class)->orderByDesc('id');
    }

    public function access()
    {
        return $this->hasMany(DocumentAccess::class);
    }

    public function favorites()
    {
        return $this->hasMany(Favorite::class);
    }
    public function reviewer()
    {
        return $this->belongsTo(User::class, 'reviewed_by');
    }
    public function getStatusLabelAttribute()
    {
        return match($this->status) {
            0 => 'Private',
            1 => 'Public',
            2 => 'Approved',
            3 => 'Rejected',
            default => 'Unknown',
        };
    }
   public function user() 
{
    // Change 'user_id' to 'created_by' as seen in your $fillable array
    return $this->belongsTo(User::class, 'created_by'); 
}
}
