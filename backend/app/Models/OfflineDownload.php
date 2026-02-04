<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class OfflineDownload extends Model
{
    use HasFactory;

    public $timestamps = false;

    protected $fillable = ['user_id', 'version_id', 'downloaded_at'];

    protected $dates = ['downloaded_at'];

    public function user()
    {
        return $this->belongsTo(User::class);
    }

    public function version()
    {
        return $this->belongsTo(DocumentVersion::class, 'version_id');
    }
}
