<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Relations\Pivot;

class UserNotification extends Pivot
{
    public $timestamps = false;

    protected $table = 'user_notifications';

    protected $fillable = ['user_id', 'notification_id', 'is_read'];

    protected $casts = [
        'is_read' => 'boolean',
    ];
}
