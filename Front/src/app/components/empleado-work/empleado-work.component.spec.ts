import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmpleadoWorkComponent } from './empleado-work.component';

describe('EmpleadoWorkComponent', () => {
  let component: EmpleadoWorkComponent;
  let fixture: ComponentFixture<EmpleadoWorkComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmpleadoWorkComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EmpleadoWorkComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
