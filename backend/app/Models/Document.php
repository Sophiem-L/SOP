<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Document extends Model
{
    use HasFactory;

    protected $fillable = ['title', 'description', 'category_id', 'created_by', 'is_active'];

    protected $casts = [
        'is_active' => 'boolean',
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
        return $this->hasMany(DocumentVersion::class);
    }

    public function access()
    {
        return $this->hasMany(DocumentAccess::class);
    }

    public function favorites()
    {
        return $this->hasMany(Favorite::class);
    }
}
