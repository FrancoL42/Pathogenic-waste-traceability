import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CloseRoadmapModalComponent } from './close-roadmap-modal.component';

describe('CloseRoadmapModalComponent', () => {
  let component: CloseRoadmapModalComponent;
  let fixture: ComponentFixture<CloseRoadmapModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CloseRoadmapModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CloseRoadmapModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
