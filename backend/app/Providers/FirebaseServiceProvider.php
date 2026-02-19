<?php

namespace App\Providers;

use Illuminate\Support\ServiceProvider;
use Kreait\Firebase\Factory;
use Kreait\Firebase\Contract\Auth;

class FirebaseServiceProvider extends ServiceProvider
{
    /**
     * Register services.
     *
     * @return void
     */
    public function register()
    {
        $this->app->singleton(Auth::class, function ($app) {
            $factory = new Factory();
            $creds = env('FIREBASE_CREDENTIALS', 'firebase_credentials.json');
            $credsPath = base_path($creds);

            try {
                if (file_exists($credsPath)) {
                    $factory = $factory->withServiceAccount($credsPath);
                } elseif (!empty(env('FIREBASE_CREDENTIALS_JSON'))) {
                    $json = json_decode(env('FIREBASE_CREDENTIALS_JSON'), true);
                    if ($json) {
                        $factory = $factory->withServiceAccount($json);
                    }
                }

                // Attempt to create Auth. This might still fail if Project ID is missing.
                return $factory->createAuth();
            } catch (\Exception $e) {
                \Log::error('Firebase initialization error: ' . $e->getMessage());

                // If it fails, we return a proxied version or a null-safe way.
                // For now, let's wrap it in a way that doesn't crash the container resolution.
                // We'll return the factory's default but catch the error if possible.
                // Actually, the best way for "Bypass" is to return null and handle in middleware,
                // but constructor injection doesn't like null.

                // Let's try to set a dummy project ID if missing to at least let it initialize
                if (str_contains($e->getMessage(), 'Project ID')) {
                    try {
                        return (new Factory)->withProjectId('dummy-project')->createAuth();
                    } catch (\Exception $e2) {
                        // Truly failed
                        throw $e;
                    }
                }
                throw $e;
            }
        });
    }

    /**
     * Bootstrap services.
     *
     * @return void
     */
    public function boot()
    {
        //
    }
}
