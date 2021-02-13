#include <stdio.h>
#include <stdint.h> // for uint8 and uint32
#include <fcntl.h>      // for open()
#include <float.h>      // for FLT_MAX
#include <sys/mman.h>   // for mmap()
#include <sys/ioctl.h>  // for framebuffer access
#include <linux/fb.h>   // for framebuffer access

typedef struct _v3_t
{
    float x, y, z;
} v3_t;

/* add two vectors */

v3_t v3_add( v3_t a , v3_t b )
{
    v3_t v;

    v.x = a.x + b.x;
    v.y = a.y + b.y;
    v.z = a.z + b.z;

    return v;
}

/* substracts b from a */

v3_t v3_sub( v3_t a , v3_t b )
{
    v3_t v;

    v.x = a.x - b.x;
    v.y = a.y - b.y;
    v.z = a.z - b.z;

    return v;
}

/* creates dot product of two vectors */

float v3_dot( v3_t a , v3_t b )
{
    return a.x * b.x + a.y * b.y + a.z * b.z;
}

/* scales vector */

v3_t v3_scale( v3_t a , float f )
{
    v3_t v;

    v.x = a.x * f;
    v.y = a.y * f;
    v.z = a.z * f;

    return v;
}

/* returns squared length to avoid square root operation */

float v3_length_squared( v3_t a )
{
    return a.x * a.x + a.y * a.y + a.z * a.z;
}

/* creates pixel color suitable for the actual screen info */

uint32_t pixel_color( uint8_t r, uint8_t g, uint8_t b, struct fb_var_screeninfo *vinfo )
{
    return  ( r << vinfo->red.offset ) |
            ( g << vinfo->green.offset ) |
            ( b << vinfo->blue.offset );
}

int main( )
{

    // get framebuffer

    struct fb_fix_screeninfo finfo;
    struct fb_var_screeninfo vinfo;

    int fb_fd = open( "/dev/fb0" , O_RDWR );

    // get and set screen information

    ioctl( fb_fd, FBIOGET_VSCREENINFO, &vinfo);

    vinfo.grayscale = 0;
    vinfo.bits_per_pixel = 32;

    ioctl( fb_fd, FBIOPUT_VSCREENINFO, &vinfo );
    ioctl( fb_fd, FBIOGET_VSCREENINFO, &vinfo );
    ioctl( fb_fd, FBIOGET_FSCREENINFO, &finfo );

    long screensize_l = vinfo.yres_virtual * finfo.line_length;

    uint8_t *fbp = mmap( 0 , screensize_l , PROT_READ | PROT_WRITE , MAP_SHARED , fb_fd , ( off_t ) 0 );

    v3_t screen_d = { vinfo.xres , vinfo.yres };    // screen dimensions

    // start ray tracing

    // set up camera

    v3_t camera_focus_p = { 0.0 , 0.0 , 100.0 };        // camera focus point

    // set up geometry

    v3_t rect_side_p = { -50.0 , 0.0 , -100.0 };    // left side center point of square
    v3_t rect_center_p = { 0 , 0 , -100.0 };        // center point of square
    v3_t rect_normal_v = { 0 , 0 , 100.0 };         // normal vector of square

    float rect_wth2_f = 50.0;                       // half width of square
    float rect_hth2_f = 40.0;                       // half height of square

    // create corresponding grid in screen and in camera window

    int grid_cols_i = 100;

    v3_t window_d = { 100.0 , 100.0 * screen_d.y / screen_d.x };    // camera window dimensions

    float screen_step_size_f = screen_d.x / grid_cols_i;    // screen block size
    float window_step_size_f = window_d.x / grid_cols_i;    // window block size

    int grid_rows_i = screen_d.y / screen_step_size_f;

    // create rays going through the camera window quad starting from the top left corner

    for ( int row_i = 0 ; row_i < grid_rows_i ; row_i++ )
    {
        for ( int col_i = 0 ; col_i < grid_cols_i ; col_i++ )
        {

            float window_grid_x = - window_d.x / 2.0 + window_step_size_f * col_i;
            float window_grid_y =   window_d.y / 2.0 - window_step_size_f * row_i;

            v3_t window_grid_v = { window_grid_x , window_grid_y , 0.0 };

            // ray/pixel location on screen

            int screen_grid_x = screen_step_size_f * col_i;
            int screen_grid_y = screen_step_size_f * row_i;

            // pixel location in framebuffer

            long location = ( screen_grid_x + vinfo.xoffset ) * ( vinfo.bits_per_pixel / 8 ) +
                            ( screen_grid_y + vinfo.yoffset ) * finfo.line_length;

            // project ray from camera focus point through window grid point to square plane
            // line - plane intersection point calculation with dot products :
            // C = A + dot(AP,N) / dot( AB,N) * AB

            v3_t AB = v3_sub( window_grid_v , camera_focus_p );
            v3_t AP = v3_sub( rect_center_p , camera_focus_p );

            float dotABN = v3_dot( AB , rect_normal_v );
            float dotAPN = v3_dot( AP , rect_normal_v );

            if ( dotABN < FLT_MIN * 10.0 || dotABN > - FLT_MIN * 10.0 )
            {
                // if dot product is not close to zero there is an intersection

                float scale_f = dotAPN / dotABN;
                v3_t isect_p = v3_add( camera_focus_p , v3_scale( AB , scale_f ) );

                // let's find if intersection point is in the rectangle

                // project intersection point to center line of rectangle
                // C = A + dot(AP,AB) / dot(AB,AB) * AB

                AB = v3_sub( rect_center_p , rect_side_p );
                AP = v3_sub( isect_p , rect_side_p );

                float dotAPAB = v3_dot( AP , AB );
                float dotABAB = v3_dot( AB , AB );

                scale_f = dotAPAB / dotABAB;

                v3_t proj_p = v3_add( rect_side_p , v3_scale( AB , scale_f ) );

                // check x and y distance of intersection point from square center

                float dist_x = v3_length_squared( v3_sub( proj_p , rect_center_p ) );
                float dist_y = v3_length_squared( v3_sub( proj_p , isect_p ) );

                // compare squared distances with squared distances of rectangle

                if ( dist_x < rect_wth2_f * rect_wth2_f &&
                     dist_y < rect_hth2_f * rect_hth2_f )
                {
                    // cross point is inside square, we draw it white

                    *((uint32_t*)(fbp + location)) = pixel_color( 0xFF, 0xFF, 0xFF, &vinfo );
                }
                else
                {
                    // cross point is outside square, we draw it blue

                    *((uint32_t*)(fbp + location)) = pixel_color( 0x00, 0x00, 0xFF , &vinfo );
                }

            }

        }

    }

    return 0;
}
