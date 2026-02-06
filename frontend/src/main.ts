import { Component } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, RouterOutlet, RouterLink } from '@angular/router';
import { routes } from './app/app.routes';
import { appConfig } from './app/app.config';
import {AppComponent} from "./app/app.component";


bootstrapApplication(AppComponent, appConfig)
    .catch((err) => console.error(err));