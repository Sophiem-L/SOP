<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Notification extends Model
{
    use HasFactory;

    protected $fillable = ['title', 'message', 'type', 'action_url', 'document_id'];

    public function document()
    {
        return $this->belongsTo(Document::class);
    }

    public function users()
    {
        return $this->belongsToMany(User::class, 'user_notifications')
            ->withPivot('is_read');
    }
}
